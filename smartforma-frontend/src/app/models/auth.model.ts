export type UserRole = 'ADMIN' | 'LEARNER';

export interface AuthUser {
  token: string;
  tokenType: string;
  compteId: number;
  email: string;
  role: UserRole;
  apprenantId: number | null;
  nom: string | null;
  prenom: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  password: string;
  competences?: string | null;
  interets?: string | null;
  niveau?: 'DEBUTANT' | 'INTERMEDIAIRE' | 'AVANCE';
}
