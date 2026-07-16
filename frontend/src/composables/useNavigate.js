export function useNavigate() {
  function go(path) {
    window.location.href = '/game-library' + (path.startsWith('/') ? path : '/' + path)
  }

  return { go }
}
