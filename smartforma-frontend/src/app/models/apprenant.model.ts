export type Niveau = 'DEBUTANT' | 'INTERMEDIAIRE' | 'AVANCE';

export interface Apprenant {
  id?: number;
  nom: string;
  prenom: string;
  email: string;
  competences?: string | null;
  interets?: string | null;
  niveau?: Niveau;
}

export interface ApprenantDto {
  nom: string;
  prenom: string;
  email: string;
  competences?: string | null;
  interets?: string | null;
  niveau?: Niveau;
}
