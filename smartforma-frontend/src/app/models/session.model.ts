import { Formation } from './formation.model';

export interface Session {
  id?: number;
  formation?: Formation;
  dateDebut: string;
  dateFin: string;
  heureDebut?: string | null;
  heureFin?: string | null;
  capacite: number;
}

export interface SessionDto {
  formationId: number;
  dateDebut: string;
  dateFin: string;
  heureDebut?: string | null;
  heureFin?: string | null;
  capacite: number;
}
