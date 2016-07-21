import React, { PropTypes } from 'react';
import Log from './Log';

const Pane = ({children}) => {
  return (
    <div className="log-pane">
      {children}
    </div>
  );
};

Pane.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.instanceOf(Log),
    PropTypes.arrayOf(PropTypes.instanceOf(Pane))
  ]).isRequired,
};

export default Pane;
