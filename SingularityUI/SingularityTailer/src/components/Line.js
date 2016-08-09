import React, { PropTypes } from 'react';

const Line = ({data}) => {
  if (data.isMissingMarker) {
    const missingBytes = data.end - data.start;
    return (
      <div className="log-row" style={{backgroundColor: data.isLoading ? 'red' : 'green' }} key={`${data.start}-${data.end}`}>
        {missingBytes} bytes
      </div>
    );
  }

  if (data.ansi) {
    const ansiStyled = data.ansi.map((part, i) => (
      <span key={i} className={null || part.classes}>
        {part.content}
      </span>
    ));

    return <div className="log-row" key={`${data.start}-${data.end}`}>{ansiStyled}</div>;
  }

  return <div className="log-row" key={`${data.start}-${data.end}`}>{data.text}</div>;
};

Line.propTypes = {
  data: PropTypes.object.isRequired
};

export default Line;
