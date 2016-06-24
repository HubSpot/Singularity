import React, { PropTypes } from 'react';

const RequestCount = ({label, count, link}) =>
  <a className="big-number-link" href={link}>
    <div className="well">
      <div className="big-number">
        <div className="number" data-state-attribute="requests">
          {count}
        </div>
        <div className="number-label">{label}</div>
      </div>
    </div>
  </a>;

RequestCount.propTypes = {
  label: PropTypes.string.isRequired,
  count: PropTypes.number.isRequired,
  link: PropTypes.string
};

export default RequestCount;
