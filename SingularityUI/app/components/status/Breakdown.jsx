import React from 'react';
import Tooltip from 'react-bootstrap/lib/Tooltip';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ReactDOM from 'react-dom';

export default class Breakdown extends React.Component {

  renderSections() {
    return this.props.data.map((d, i) => {
      return (
        <a key={i} href={`${config.appRoot}${d.link}`}>
          <OverlayTrigger rootClose placement="right" overlay={<Tooltip id={d.attribute}>{`${d.count} ${d.label}`}</Tooltip>}>
            <span
              data-type="column"
              data-state-attribute={d.attribute}
              style={{height: `${d.percent}%`}}
              className={`chart__data-point chart-fill-${d.type}`}
              data-original-title={`${d.count} ${d.label}`} />
          </OverlayTrigger>
        </a>
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
