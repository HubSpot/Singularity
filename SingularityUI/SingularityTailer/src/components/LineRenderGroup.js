import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';

import Line from './Line';

class LineRenderGroup extends Component {
  shouldComponentUpdate(nextProps) {
    return nextProps.lines !== this.props.lines;
  }

  render() {
    return (
      <div className="render-group">
        {this.props.lines.map((data) => {
          return (
            <Line
              key={`${data.start}-${data.end}`}
              data={data}
              hrefFunc={this.props.hrefFunc}
            />
          );
        })}
      </div>
    );
  }
}

LineRenderGroup.propTypes = {
  lines: PropTypes.object.isRequired,
  hrefFunc: PropTypes.func
};

export default LineRenderGroup;
