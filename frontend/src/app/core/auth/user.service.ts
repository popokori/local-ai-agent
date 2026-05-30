import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { UserDto } from './auth.types';

export interface UpdateUserRequest {
  displayName?: string;
  email?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = `${environment.apiUrl}/users/me`;

  async updateMe(request: UpdateUserRequest): Promise<UserDto> {
    const updated = await firstValueFrom(
      this.http.patch<UserDto>(this.base, request),
    );
    // Le AuthService expose `user` via signal — on rafraîchit le profil
    await this.auth.loadProfile().catch(() => updated);
    return updated;
  }

  async changePassword(request: ChangePasswordRequest): Promise<void> {
    await firstValueFrom(
      this.http.post<void>(`${this.base}/password`, request),
    );
  }
}
