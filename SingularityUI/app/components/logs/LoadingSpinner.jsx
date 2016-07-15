import React from 'react';
import classNames from 'classnames';

function LoadingSpinner(props) {
  let className = classNames({
    'page-loader': true,
    centered: this.props.centered
  });

  if (this.props.children.length > 0) {
    return <div className="page-loader-with-message"><div className={className} /><p>{props.children}</p></div>;
  }
  return <div className={className} />;
}

LoadingSpinner.propTypes = {
  text: React.PropTypes.string,
  centered: React.PropTypes.bool
};

export default LoadingSpinner;

