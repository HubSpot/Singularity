import React, { PropTypes } from 'react';
import { Link } from 'react-router';

const RequestCount = ({label, count, link}) =>
  <Link className="big-number-link" to={link}>
    <div className="well">
      <div className="big-number">
        <div className="number" data-state-attribute="requests">
          {count}
        </div>
        <div className="number-label">{label}</div>
      </div>
    </div>
  </Link>;

RequestCount.propTypes = {
  label: PropTypes.string.isRequired,
  count: PropTypes.number.isRequired,
  link: PropTypes.string
};

export default RequestCount;
