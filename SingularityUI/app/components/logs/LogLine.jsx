import React from 'react';
import classNames from 'classnames';
import ansiStyleParser from 'ansi-style-parser';
import 'styles/scss/ansi-log-styles';

import { connect } from 'react-redux';
import { clickPermalink } from '../../actions/log';

class LogLine extends React.Component {
  highlightContent(content) {
    const { search } = this.props;
    if (!search || _.isEmpty(search)) {
      return ansiStyleParser(content).map((p, i) => {
        return <span key={i} className={p.styles}>{p.text}</span>;
      });
    }

    const regex = RegExp(search, 'g');
    const matches = [];

    for (let match = regex.exec(content); match; match = regex.exec(content)) {
      matches.push(match);
    }

    const sections = [];
    let lastEnd = 0;
    for (let i = 0; i < matches.length; i++) {
      const match = matches[i];
      const last = {
        text: content.slice(lastEnd, match.index),
        match: false
      };
      const sect = {
        text: content.slice(match.index, match.index + match[0].length),
        match: true
      };
      sections.push(last, sect);
      lastEnd = match.index + match[0].length;
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
    const divClass = classNames({
      line: true,
      highlightLine: this.props.isHighlighted
    });

    return (
      <div className={divClass} style={{ backgroundColor: this.props.color }}>
        <a
          href={`${ config.appRoot }/task/${ this.props.taskId }/tail/${ this.props.path }#${ this.props.offset }`}
          className="offset-link"
          onClick={() => this.props.clickPermalink(this.props.offset)}
        >
          <div className="pre-line">
            <span className="glyphicon glyphicon-link" data-offset={`${ this.props.offset }`} />
          </div>
        </a>
        <span>
          {this.highlightContent(this.props.content)}
        </span>
      </div>
    );
  }
}

LogLine.propTypes = {
  offset: React.PropTypes.number.isRequired,
  isHighlighted: React.PropTypes.bool.isRequired,
  content: React.PropTypes.string.isRequired,
  taskId: React.PropTypes.string.isRequired,
  showDebugInfo: React.PropTypes.bool,
  color: React.PropTypes.string,
  timestamp: React.PropTypes.number,
  path: React.PropTypes.string,
  search: React.PropTypes.string,
  clickPermalink: React.PropTypes.func.isRequired
};

const mapStateToProps = (state) => ({
  search: state.search,
  showDebugInfo: state.showDebugInfo,
  path: state.path
});

const mapDispatchToProps = { clickPermalink };

export default connect(mapStateToProps, mapDispatchToProps)(LogLine);

