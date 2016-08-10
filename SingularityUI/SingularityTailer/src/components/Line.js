import React, { PropTypes } from 'react';
import classNames from 'classnames';


const Line = ({data}) => {
  let lineContents;

  const classes = classNames({
    'log-row': true,
    'missing': data.isMissingMarker,
    'loading': data.isLoading
  });

  if (data.isMissingMarker) {
    const missingBytes = data.end - data.start;
    lineContents = <span>{missingBytes} bytes</span>;
  }

  if (data.ansi) {
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
    <div className={classes} key={`${data.start}-${data.end}`}>
      {lineContents}
    </div>
  );
};

Line.propTypes = {
  data: PropTypes.object.isRequired
};

export default Line;
