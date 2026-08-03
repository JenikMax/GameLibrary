import DOMPurify from 'dompurify'

const purify = DOMPurify()

export function sanitizeHtml(html) {
  if (!html) return ''
  return purify.sanitize(html, { ALLOWED_TAGS: purify.sanitize.DEFAULT_ALLOWED_TAGS, ALLOWED_ATTR: purify.sanitize.DEFAULT_ALLOWED_ATTR })
}
