import React from 'react';
import { Link } from 'react-router';
import Tooltip from 'react-bootstrap/lib/Tooltip';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';

export default class Breakdown extends React.Component {

  renderSections() {
    return this.props.data.map((item, key) => {
      return (
        <Link key={key} to={item.link}>
          <OverlayTrigger rootClose={true} placement="right" overlay={<Tooltip id={item.attribute}>{`${item.count} ${item.label}`}</Tooltip>}>
            <span
              data-type="column"
              data-state-attribute={item.attribute}
              style={{height: `${item.percent}%`}}
              className={`chart__data-point chart-fill-${item.type}`}
              data-original-title={`${item.count} ${item.label}`}
            />
          </OverlayTrigger>
        </Link>
      );
    });
  }

  render() {
    return (
      <div>
        <div className="chart__column">
          {this.renderSections()}
        </div>
        <h5 className="text-center">{this.props.total} Total</h5>
      </div>
    );
  }
}

Breakdown.propTypes = {
  total: React.PropTypes.number.isRequired,
  data: React.PropTypes.arrayOf(React.PropTypes.shape({
    count: React.PropTypes.number.isRequired,
    type: React.PropTypes.string.isRequired,
    label: React.PropTypes.string.isRequired,
    link: React.PropTypes.string.isRequired,
    percent: React.PropTypes.number.isRequired
  })).isRequired
};
