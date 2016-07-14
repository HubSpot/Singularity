import React, { PropTypes } from 'react';

const Section = ({id, title, children}) => (
  <div id={id}>
    <div className="page-header">
      <h2>{title}</h2>
    </div>
    {children}
  </div>
);

Section.propTypes = {
  id: PropTypes.string,
  title: PropTypes.oneOfType([
    PropTypes.node,
    PropTypes.arrayOf(PropTypes.node),
    PropTypes.string
  ]).isRequired,
  children: PropTypes.oneOfType([
    PropTypes.node,
    PropTypes.arrayOf(PropTypes.node)
  ])
};

export default Section;
