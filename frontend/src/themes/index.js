import defaultLight from './default-light'
import defaultDark from './default-dark'
import retroTerminal from './retro-terminal'
import yellowedCrt from './yellowed-crt'

export const themes = [defaultLight, defaultDark, retroTerminal, yellowedCrt]
export const themesById = Object.fromEntries(themes.map(t => [t.id, t]))
export const DEFAULT_THEME = 'default-light'
