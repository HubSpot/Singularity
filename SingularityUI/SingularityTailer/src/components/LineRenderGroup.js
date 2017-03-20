import React, { Component, PropTypes } from 'react';

import Line from './Line';

class LineRenderGroup extends Component {
  shouldComponentUpdate(nextProps) {
    return nextProps.lines.size !== this.props.lines.size
      || nextProps.lines.first().start !== this.props.lines.first().start
      || nextProps.lines.first().isMissingMarker !== this.props.lines.first().isMissingMarker
      || nextProps.lines.last().end !== this.props.lines.last().end
      || nextProps.lines.last().isMissingMarker !== this.props.lines.last().isMissingMarker;
  }

  renderLines() {
    return this.props.lines.map((data) => {
      return (
        <Line
          key={`${data.start}-${data.end}`}
          data={data}
          highlighted={this.props.highlightedOffset === data.start}
          lineLinkRenderer={this.props.lineLinkRenderer}
        />
      );
    })
  }

  render() {
    return (
      <div className="render-group">
        {this.renderLines()}
      </div>
    );
  }
}

LineRenderGroup.propTypes = {
  lines: PropTypes.object.isRequired,
  highlightedOffset: PropTypes.number,
  lineLinkRenderer: PropTypes.func
};

export default LineRenderGroup;
