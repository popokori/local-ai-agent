package mr.popo.localaiagent.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.common.exception.BusinessException;
import mr.popo.localaiagent.common.exception.ResourceNotFoundException;
import mr.popo.localaiagent.common.pagination.PageResponse;
import mr.popo.localaiagent.document.domain.Document;
import mr.popo.localaiagent.document.domain.DocumentStatus;
import mr.popo.localaiagent.document.domain.KnowledgeBase;
import mr.popo.localaiagent.document.dto.DocumentDto;
import mr.popo.localaiagent.document.mapper.DocumentMapper;
import mr.popo.localaiagent.document.repository.DocumentRepository;
import mr.popo.localaiagent.rag.vector.VectorStoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseService kbService;
    private final DocumentIngestionService ingestionService;
    private final VectorStoreService vectorStore;
    private final DocumentMapper mapper;

    @Value("${app.rag.upload-dir:./data/uploads}")
    private String uploadDirRoot;

    @Override
    @Transactional
    public DocumentDto upload(Long ownerId, Long kbId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Empty file");
        }
        KnowledgeBase kb = kbService.loadOwned(kbId, ownerId);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("Cannot read upload: " + ex.getMessage());
        }
        String sha256 = sha256Hex(content);

        Path storagePath = persistFileToDisk(ownerId, kbId, file.getOriginalFilename(), content);

        Document doc = new Document();
        doc.setKbId(kb.getId());
        doc.setOwnerId(ownerId);
        doc.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
        doc.setMimeType(file.getContentType());
        doc.setSizeBytes((long) content.length);
        doc.setSha256(sha256);
        doc.setStoragePath(storagePath.toString());
        doc.setStatus(DocumentStatus.UPLOADED);
        doc = documentRepository.saveAndFlush(doc);

        // Ingestion en arrière-plan sur virtual thread
        ingestionService.ingest(doc.getId());

        return mapper.toDto(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentDto> list(Long ownerId, Long kbId, Pageable pageable) {
        kbService.loadOwned(kbId, ownerId);
        Page<Document> page = documentRepository
                .findAllByKbIdAndOwnerIdOrderByCreatedAtDesc(kbId, ownerId, pageable);
        return PageResponse.of(page.map(mapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto get(Long ownerId, Long documentId) {
        return mapper.toDto(loadOwned(ownerId, documentId));
    }

    @Override
    @Transactional
    public void delete(Long ownerId, Long documentId) {
        Document doc = loadOwned(ownerId, documentId);
        // Règle DESIGN_RULES.md §3 : on supprime le vector store AVANT la DB
        // pour ne pas laisser de chunks orphelins en cas d'échec.
        vectorStore.deleteByDocumentId(ownerId, documentId);
        // Tentative best-effort de nettoyer le fichier sur disque
        try {
            Files.deleteIfExists(Path.of(doc.getStoragePath()));
        } catch (IOException ex) {
            log.warn("Could not delete uploaded file at {}: {}", doc.getStoragePath(), ex.getMessage());
        }
        documentRepository.delete(doc);
    }

    private Document loadOwned(Long ownerId, Long documentId) {
        return documentRepository.findByIdAndOwnerId(documentId, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
    }

    private Path persistFileToDisk(Long ownerId, Long kbId, String originalName, byte[] content) {
        try {
            String safeName = (originalName == null ? "upload" : originalName)
                    .replaceAll("[^A-Za-z0-9._-]+", "_");
            Path dir = Path.of(uploadDirRoot, String.valueOf(ownerId), String.valueOf(kbId));
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "_" + safeName);
            Files.write(target, content);
            return target;
        } catch (IOException ex) {
            throw new BusinessException("Failed to persist file: " + ex.getMessage());
        }
    }

    private static String sha256Hex(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
