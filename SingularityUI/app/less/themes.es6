
/** @type {import("xterm").ITheme} */
const DEFAULT = {
  background: '#ffffff',
  foreground: '#000000',
  selection: '#dddddd',
  cursor: "#000000",  
};

// xterm's default theme is dark, but
const DARK = {
  background: '#000000',
  foreground: '#ffffff',
  selection: '#dddddd',
  cursor: "#ffffff",  
};

export const THEMES = {
  default: DEFAULT,
  light: DEFAULT,
  dark: DARK, 
}
