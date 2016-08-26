import React, { Component, PropTypes } from 'react';
import escapeHtml from 'escape-html';
import classNames from 'classnames';

const createMarkup = (lines, hrefFunc) => {
  const htmlLines = lines.map((data) => {
    let lineContents;

    const classes = escapeHtml(classNames({
      'log-row': true,
      'missing': data.isMissingMarker,
      'loading': data.isLoading
    }));

    let maybeHref = '';

    if (data.isMissingMarker) {
      const missingBytes = data.end - data.start;
      lineContents = `<span>${escapeHtml(missingBytes)} bytes</span>`;
    } else if (data.ansi) {
      const ansiStyled = data.ansi.map((part) => (
        `<span ${part.classes ? escapeHtml(part.classes) : ''}>${escapeHtml(part.content)}</span>`
      ));

      lineContents = ansiStyled;
    } else {
      lineContents = escapeHtml(data.text);
    }

    if (hrefFunc && !data.isMissingMarker) {
      const href = escapeHtml(hrefFunc(data.start));
      maybeHref = `<a class="line-link" href="${href}">@</a>`;
    }

    return `<div class="${classes}">${maybeHref}${lineContents}</div>`;
  });

  return {
    __html: htmlLines.join('')
  };
};

// why? React reconciliation times are way slower than just generating the html
// and setting innerHtml
export default class DangerousLineRenderGroup extends Component {
  shouldComponentUpdate(nextProps) {
    return nextProps.lines !== this.props.lines;
  }

  render() {
    const markup = createMarkup(this.props.lines, this.props.hrefFunc);
    return (
      <div
        className="render-group"
        dangerouslySetInnerHTML={markup}
      />
    );
  }
}

DangerousLineRenderGroup.propTypes = {
  lines: PropTypes.object.isRequired,
  hrefFunc: PropTypes.func
};
