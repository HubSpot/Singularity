import React, { PropTypes } from 'react';

const Loader = ({fixed}) => (
  <div className={`page-loader ${fixed ? 'fixed' : ''}`} />
);

Loader.propTypes = {
  fixed: PropTypes.bool
};

Loader.defaultProps = {
  fixed: true
};

export default Loader;
