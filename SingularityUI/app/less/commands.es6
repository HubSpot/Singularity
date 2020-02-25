import { Terminal } from 'xterm';

/** @param {Terminal} terminal */
export function sigint(terminal) {
  terminal.paste('\x03');
}

/** @param {Terminal} terminal */
export function jumpToTop(terminal) {
  sigint(terminal);
  terminal.paste('\rg');
}

/** @param {Terminal} terminal */
export function jumpToBottom(terminal) {
  sigint(terminal);
  terminal.paste('\rG');
}
