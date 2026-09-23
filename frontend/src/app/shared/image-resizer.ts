import { Injectable } from '@angular/core';

/**
 * Réduit une image dans le navigateur avant l'envoi (JPEG, 800 px au plus) : envoi rapide même depuis un téléphone,
 * et prise en charge des formats que le navigateur sait afficher (HEIC sur Safari, WebP…). Le serveur revérifie et
 * réencode l'image de toute façon.
 */
@Injectable({ providedIn: 'root' })
export class ImageResizer {
  static readonly MAX_SIZE = 800;

  async toJpeg(file: Blob, maxSize = ImageResizer.MAX_SIZE): Promise<Blob> {
    const bitmap = await createImageBitmap(file); // lève une erreur si le navigateur ne sait pas lire l'image
    try {
      const ratio = Math.min(1, maxSize / Math.max(bitmap.width, bitmap.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(bitmap.width * ratio));
      canvas.height = Math.max(1, Math.round(bitmap.height * ratio));
      const context = canvas.getContext('2d');
      if (!context) {
        throw new Error('Canvas indisponible');
      }
      context.fillStyle = '#fff'; // fond des images transparentes
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
      return await new Promise<Blob>((resolve, reject) =>
        canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Encodage impossible'))), 'image/jpeg', 0.85),
      );
    } finally {
      bitmap.close();
    }
  }
}
