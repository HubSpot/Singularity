import { Terminal } from 'xterm';

/** @param {Terminal} terminal */
export function jumpToTop(terminal) {
  terminal.paste('\rg');
}

/** @param {Terminal} terminal */
export function jumpToBottom(terminal) {
  terminal.paste('\rG');
}
