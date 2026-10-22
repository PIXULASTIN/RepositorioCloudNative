/** Convierte un error HTTP en un mensaje legible (401 / 403 / 409 / mensaje del backend). */
export function errorMsg(err: any): string {
  switch (err?.status) {
    case 0: return 'No se pudo conectar con la API (revisa URL, CORS o que el backend este arriba).';
    case 401: return 'No autenticado (401): falta token o es invalido.';
    case 403: return 'Sin permiso (403): tu rol no puede ejecutar esta operacion.';
  }
  return err?.error?.mensaje || err?.message || 'Error inesperado';
}
