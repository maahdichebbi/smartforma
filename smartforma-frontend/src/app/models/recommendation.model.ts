import { Formation } from './formation.model';

export interface ScoreCriteria {
  scoreCompetences: number;
  scoreInterets: number;
  scoreNiveau: number;
}

export interface Recommendation {
  formation: Formation;
  score: number;
  explications: string[];
  criteres: ScoreCriteria;
}
