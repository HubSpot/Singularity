import React from 'react';
import classNames from 'classnames';

function LoadingSpinner(props) {
  let className = classNames({
    'page-loader': true,
    centered: props.centered
  });

  if (props.children.length > 0) {
    return <div className="page-loader-with-message"><div className={className} /><p>{props.children}</p></div>;
  }
  return <div className={className} />;
}

LoadingSpinner.propTypes = {
  text: React.PropTypes.string,
  centered: React.PropTypes.bool,
  children: React.PropTypes.node
};

export default LoadingSpinner;

