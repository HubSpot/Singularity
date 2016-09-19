let m;
import React from 'react';
import classNames from 'classnames';

import { connect } from 'react-redux';
import { clickPermalink } from '../../actions/log';

class LogLine extends React.Component {
  highlightContent(content) {
    let { search } = this.props;
    if (!search || _.isEmpty(search)) {
      if (this.props.showDebugInfo) {
        return `${ this.props.offset } | ${ this.props.timestamp } | ${ content }`;
      } else {
        return content;
      }
    }

    let regex = RegExp(search, 'g');
    let matches = [];

    while (m = regex.exec(content)) {
      matches.push(m);
    }

    let sections = [];
    let lastEnd = 0;
    for (let i = 0; i < matches.length; i++) {
      var m = matches[i];
      let last = {
        text: content.slice(lastEnd, m.index),
        match: false
      };
      let sect = {
        text: content.slice(m.index, m.index + m[0].length),
        match: true
      };
      sections.push(last, sect);
      lastEnd = m.index + m[0].length;
    }
    sections.push({
      text: content.slice(lastEnd),
      match: false
    });

    return sections.map((s, i) => {
      let spanClass = classNames({ 'search-match': s.match });
      return <span key={i} className={spanClass}>{s.text}</span>;
    });
  }

  render() {
    let divClass = classNames({
      line: true,
      highlightLine: this.props.isHighlighted
    });

    let linkStyle;
    if (this.props.compressedLog) {
      linkStyle = "view";
    } else {
      linkStyle = "tail";
    }

    return React.createElement("div", { "className": divClass, "style": { backgroundColor: this.props.color } }, React.createElement("a", { "href": `${ config.appRoot }/task/${ this.props.taskId }/${linkStyle}/${ this.props.path }#${ this.props.offset }`, "className": "offset-link", ["onClick"]: () => this.props.clickPermalink(this.props.offset) }, <div className="pre-line"><span className="glyphicon glyphicon-link" data-offset={`${ this.props.offset }`} /></div>), <span>{this.highlightContent(this.props.content)}</span>);
  }
}

LogLine.propTypes = {
  offset: React.PropTypes.number.isRequired,
  compressedLog: React.PropTypes.bool.isRequired,
  isHighlighted: React.PropTypes.bool.isRequired,
  content: React.PropTypes.string.isRequired,
  taskId: React.PropTypes.string.isRequired,
  showDebugInfo: React.PropTypes.bool,
  color: React.PropTypes.string,

  search: React.PropTypes.string,
  clickPermalink: React.PropTypes.func.isRequired
};

let mapStateToProps = (state, ownProps) => ({
  search: state.search,
  showDebugInfo: state.showDebugInfo,
  path: state.path
});

let mapDispatchToProps = { clickPermalink };

export default connect(mapStateToProps, mapDispatchToProps)(LogLine);

