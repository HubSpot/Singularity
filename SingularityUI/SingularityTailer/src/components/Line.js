import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';

const LineLink = ({start}) => {

};

export default class Line extends Component {
  shouldComponentUpdate(nextProps) {
    return (
      this.props.data.isMissingMarker !== nextProps.data.isMissingMarker
      || this.props.data.isLoading !== nextProps.data.isLoading
      || this.props.data.start !== nextProps.data.start
      || this.props.data.end !== nextProps.data.end
    );
  }

  render() {
    const { data } = this.props;
    let lineContents;

    const classes = classNames({
      'log-row': true,
      'missing': data.isMissingMarker,
      'loading': data.isLoading
    });

    if (data.isMissingMarker) {
      const missingBytes = data.end - data.start;
      lineContents = <span>{missingBytes} bytes</span>;
    } else if (data.ansi) {
      const ansiStyled = data.ansi.map((part, i) => (
        <span key={i} className={null || part.classes}>
          {part.content}
        </span>
      ));

      lineContents = ansiStyled;
    } else {
      lineContents = data.text;
    }

    return (
      <div className={classes}>
        {lineContents}
      </div>
    );
  }
}

Line.propTypes = {
  data: PropTypes.object.isRequired
};

export default Line;
