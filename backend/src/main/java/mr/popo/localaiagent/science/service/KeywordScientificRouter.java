package mr.popo.localaiagent.science.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mr.popo.localaiagent.science.api.Domain;
import mr.popo.localaiagent.science.api.ScientificRouter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Détection de domaine rapide à partir de mots-clés et regex. Couvre 90% des cas
 * en évitant un appel LLM. Phase 4.x pourra ajouter un classifieur LLM en
 * fallback derrière un cache LRU.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class KeywordScientificRouter implements ScientificRouter {

    /** Indice d'une formule chimique (H2O, NaCl, C6H12O6…). */
    private static final Pattern CHEM_FORMULA = Pattern.compile("\\b[A-Z][a-z]?(\\d+)?([A-Z][a-z]?\\d*)+\\b");
    /** Indice d'une équation / formule math notable. */
    private static final Pattern MATH_SIGN = Pattern.compile("[∑∫√≠≤≥π∂∞]|\\\\frac|\\\\int|\\\\sum");
    /** Codes ATC, DCI typiques en majuscules + tirets (très approximatif). */
    private static final Pattern DRUG_HINT = Pattern.compile("\\b(?:mg|mcg|UI|mmol|mEq|posologie)\\b", Pattern.CASE_INSENSITIVE);

    private static final Map<Domain, Set<String>> KEYWORDS = Map.of(
            Domain.BIOLOGY, Set.of(
                    "adn", "arn", "génome", "gene", "cellule", "mitose", "méiose", "protéine",
                    "enzyme", "bactérie", "virus", "chromosome", "ribosome", "phylogénie"),
            Domain.CHEMISTRY, Set.of(
                    "molécule", "atome", "isotope", "oxydation", "réduction", "ph",
                    "stoechiométrie", "molaire", "covalente", "ionique", "acide", "base"),
            Domain.MEDICAL, Set.of(
                    "symptôme", "diagnostic", "pathologie", "syndrome", "traitement",
                    "posologie", "contre-indication", "anamnèse", "médecin", "patient"),
            Domain.MATHEMATICS, Set.of(
                    "intégrale", "dérivée", "matrice", "vecteur", "tenseur", "topologie",
                    "polynôme", "équation différentielle", "limite", "théorème"),
            Domain.COMPUTER_SCIENCE, Set.of(
                    "algorithme", "complexité", "compilateur", "kubernetes", "docker",
                    "framework", "java", "python", "spring", "rest", "http", "sql"),
            Domain.SCIENCE, Set.of(
                    "physique", "quantique", "relativité", "thermodynamique", "entropie",
                    "énergie", "champ", "particule", "boson", "fermion"));

    @Override
    public Domain detect(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return Domain.GENERIC;
        String lower = userMessage.toLowerCase(Locale.ROOT);

        // Signes forts (regex) prioritaires
        if (CHEM_FORMULA.matcher(userMessage).find()) return Domain.CHEMISTRY;
        if (MATH_SIGN.matcher(userMessage).find()) return Domain.MATHEMATICS;
        if (DRUG_HINT.matcher(lower).find()) return Domain.MEDICAL;

        // Comptage de mots-clés par domaine
        Domain best = Domain.GENERIC;
        int bestScore = 0;
        for (Map.Entry<Domain, Set<String>> entry : KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }
        if (bestScore == 0) return Domain.GENERIC;
        log.debug("Detected domain {} (score={}) for '{}'", best, bestScore, abbreviate(userMessage));
        return best;
    }

    private static String abbreviate(String s) {
        return s.length() <= 80 ? s : s.substring(0, 77) + "...";
    }
}
