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

/** @param {Terminal} terminal */
export function getTerminalText(terminal) {
  let text = '';
  for (let i = 0; i < terminal.buffer.length; i++) {
    const line = terminal.buffer.getLine(i);
    for (let j = 0; j < line.length; j++) {
      const cell = line.getCell(j);
      text += cell.getChars();
    }
  }
  return text;
}

/**
 * @param {Terminal} terminal
 * @param {WheelEvent} event
 */
export function horizontalScroll(terminal, event) {
  event.preventDefault();

  if (Math.abs(event.deltaX) <= 2) {
    return;
  } else if (event.deltaX > 0) {
    terminal.paste(`${event.deltaX}\x1bOC`);
  } else {
    terminal.paste(`${event.deltaX}\x1bOD`);
  }
}
