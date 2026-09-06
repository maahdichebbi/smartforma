export interface Chapitre {
  id?: number;
  titre: string;
  description?: string | null;   // TEXT column, can be null
  ordre: number;                  // ordering index (1-based)
  // Note: Chapitre entity does NOT have a dureeMinutes field — removed
}

export interface ChapitreDto {
  titre: string;
  description?: string | null;
  ordre: number;
  formationId?: number;
}
