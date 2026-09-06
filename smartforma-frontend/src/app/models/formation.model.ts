import { Categorie } from './categorie.model';
import { Chapitre } from './chapitre.model';

export type Niveau = 'DEBUTANT' | 'INTERMEDIAIRE' | 'AVANCE';

export interface Formation {
  id?: number;
  titre: string;
  description: string;
  niveau: Niveau;
  prix: number;          // BigDecimal serialized as number
  dureeHeures?: number;  // Integer, duration in hours
  categorie?: Categorie;
  chapitres?: Chapitre[];
  tags?: string;
}

export interface FormationDto {
  titre: string;
  description: string;
  niveau: Niveau;
  prix: number;
  dureeHeures?: number;
  categorieId: number;
  tags?: string;
}
