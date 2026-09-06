import { Apprenant } from './apprenant.model';
import { Session } from './session.model';

export type StatutInscription = 'EN_ATTENTE' | 'CONFIRMEE' | 'ANNULEE';

export interface Inscription {
  id?: number;
  apprenant?: Apprenant;
  session?: Session;
  dateInscription?: string;
  dateHeureInscription?: string;
  statut: StatutInscription;
  positionFile?: number | null;
  message?: string;
  messagePromotion?: string;
}

export interface InscriptionDto {
  apprenantId: number;
}

export interface SessionStats {
  sessionId: number;
  capacite: number;
  placesOccupees: number;
  placesDisponibles: number;
  nombreEnAttente: number;
  estComplete: boolean;
}
