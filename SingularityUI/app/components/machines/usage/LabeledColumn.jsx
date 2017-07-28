import React, { PropTypes } from 'react';

const LabeledSection = ({title, width, children, className}) => (
  <div className={`col-xs-${width} ${className || ''}`}>
    <h4>{title}</h4>
    <div className="row">
      <div className="col-xs-12">
        {children}
      </div>
    </div>
  </div>
);

LabeledSection.propTypes = {
  title: PropTypes.string,
  width: PropTypes.number.isRequired,
  children: React.PropTypes.oneOfType([
    React.PropTypes.arrayOf(React.PropTypes.node),
    React.PropTypes.node
  ]).isRequired,
  className: PropTypes.string
};

export default LabeledSection;
