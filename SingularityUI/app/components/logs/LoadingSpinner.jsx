import React from 'react';
import classNames from 'classnames';

class LoadingSpinner extends React.Component {
  render() {
    let className = classNames({
      'page-loader': true,
      centered: this.props.centered
    });

    if (this.props.children.length > 0) {
      return <div className="page-loader-with-message"><div className={className} /><p>{this.props.children}</p></div>;
    } else {
      return <div className={className} />;
    }
  }
}

LoadingSpinner.propTypes = {
  text: React.PropTypes.string,
  centered: React.PropTypes.bool
};

export default LoadingSpinner;

