import React, { PropTypes } from 'react';

const RequestCounts = ({children}) =>
  <div className="row">
    <div className="col-md-12">
      <div className="page-header">
        <h2>My requests</h2>
      </div>
      <div className="row">
        {children.map((well) => <div key={well.props.label} className="col-md-2">{well}</div>)}
      </div>
    </div>
  </div>;

RequestCounts.propTypes = {
  children: PropTypes.arrayOf(PropTypes.node)
};

export default RequestCounts;
