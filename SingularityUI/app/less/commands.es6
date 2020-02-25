import { Terminal } from 'xterm';

/** @param {Terminal} terminal @param {any[]} commands */
export function chain(terminal, commands) {
  if (!commands || commands.length <= 0) {
    return;
  }

  commands[0](terminal);

  var i = 1;
  const eve = terminal.onRender(() => {
    console.log(i);
    if (i >= commands.length) {
      return eve.dispose();
    }

    commands[i](terminal);
    i++;
  });
}

/** @param {Terminal} terminal */
export function sigint(terminal) {
  terminal.paste('\x03');
}

/** @param {Terminal} terminal */
export function disableLineNumbers(terminal) {
  sigint(terminal);
  terminal.paste('-N\r');
}

/** @param {Terminal} terminal */
export function toggleLineWrapping(terminal) {
  sigint(terminal);
  terminal.paste('\r-S\r');
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
