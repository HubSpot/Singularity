import React from 'react';
import { Link } from 'react-router';
import Tooltip from 'react-bootstrap/lib/Tooltip';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';

const Breakdown = ({total, data}) => {

  const sections = data.map((item, key) => {
    return (
      <Link key={key} to={item.link}>
        <OverlayTrigger rootClose={true} placement="right" overlay={<Tooltip id={item.attribute}>{`${item.count} ${item.label}`}</Tooltip>}>
          <span
            data-type="column"
            data-state-attribute={item.attribute}
            style={{height: `${item.percent}%`}}
            className={`chart__data-point bg-${item.type}`}
            data-original-title={`${item.count} ${item.label}`}
          />
        </OverlayTrigger>
      </Link>
    );
  });

  return (
    <div>
      <div className="chart__column">
        {sections}
      </div>
      <h5 className="text-center">{total} Total</h5>
    </div>
  );
};

Breakdown.propTypes = {
  total: React.PropTypes.number.isRequired,
  data: React.PropTypes.arrayOf(React.PropTypes.shape({
    attribute: React.PropTypes.string.isRequired,
    count: React.PropTypes.number.isRequired,
    type: React.PropTypes.string.isRequired,
    label: React.PropTypes.string.isRequired,
    link: React.PropTypes.string,
    percent: React.PropTypes.number.isRequired
  })).isRequired
};

export default Breakdown;
