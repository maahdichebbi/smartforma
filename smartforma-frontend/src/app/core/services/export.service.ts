import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ToastService } from './toast.service';

@Injectable({
  providedIn: 'root'
})
export class ExportService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly baseUrl = `${environment.apiUrl}/exports`;

  /**
   * Export Session Attendance Sheet in PDF format.
   */
  exportSessionAttendancePdf(sessionId: number): Observable<HttpResponse<Blob>> {
    return this.download(`${this.baseUrl}/sessions/${sessionId}/attendance/pdf`, `emargement-session-${sessionId}.pdf`);
  }

  /**
   * Export Session Attendance Sheet in Excel format.
   */
  exportSessionAttendanceExcel(sessionId: number): Observable<HttpResponse<Blob>> {
    return this.download(`${this.baseUrl}/sessions/${sessionId}/attendance/excel`, `emargement-session-${sessionId}.xlsx`);
  }

  /**
   * Export Global Sessions Planning in Excel format.
   */
  exportSessionsExcel(): Observable<HttpResponse<Blob>> {
    return this.download(`${this.baseUrl}/sessions/excel`, 'planning-sessions.xlsx');
  }

  /**
   * Export Global Inscriptions in Excel format.
   */
  exportInscriptionsExcel(statut?: string): Observable<HttpResponse<Blob>> {
    const url = statut ? `${this.baseUrl}/inscriptions/excel?statut=${statut}` : `${this.baseUrl}/inscriptions/excel`;
    return this.download(url, 'registre-inscriptions.xlsx');
  }

  /**
   * Export Global Apprenants Roster in Excel format.
   */
  exportApprenantsExcel(): Observable<HttpResponse<Blob>> {
    return this.download(`${this.baseUrl}/apprenants/excel`, 'roster-apprenants.xlsx');
  }

  /**
   * Export Training Catalog Summary Report in PDF format.
   */
  exportCatalogPdf(): Observable<HttpResponse<Blob>> {
    return this.download(`${this.baseUrl}/formations/catalog/pdf`, 'rapport-catalogue.pdf');
  }

  /**
   * Internal generic download method with browser file trigger.
   */
  private download(url: string, fallbackFilename: string): Observable<HttpResponse<Blob>> {
    return this.http.get(url, {
      responseType: 'blob',
      observe: 'response'
    }).pipe(
      tap({
        next: (response) => {
          this.triggerBrowserDownload(response, fallbackFilename);
          this.toast.success(`Fichier téléchargé : ${this.extractFilename(response, fallbackFilename)}`);
        },
        error: (err) => {
          console.error('Export download failed', err);
          this.toast.error('Échec du téléchargement du document.');
        }
      })
    );
  }

  /**
   * Triggers download dialog in browser.
   */
  private triggerBrowserDownload(response: HttpResponse<Blob>, fallbackFilename: string): void {
    if (!response.body) return;

    const filename = this.extractFilename(response, fallbackFilename);
    const blob = new Blob([response.body], {
      type: response.headers.get('content-type') || 'application/octet-stream'
    });

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  /**
   * Extracts filename from Content-Disposition header if available.
   */
  private extractFilename(response: HttpResponse<Blob>, fallbackFilename: string): string {
    const disposition = response.headers.get('content-disposition');
    if (disposition && disposition.includes('filename=')) {
      const match = disposition.match(/filename="?([^";]+)"?/);
      if (match && match[1]) {
        return match[1].trim();
      }
    }
    return fallbackFilename;
  }
}
