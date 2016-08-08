import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Row, Col } from 'react-bootstrap';

import RequestTitle from './header/RequestTitle';
import RequestActionButtons from './header/RequestActionButtons';
import RequestAlerts from './header/RequestAlerts';
import Breadcrumbs from '../common/Breadcrumbs';

const RequestHeader = ({requestId, group}) => {
  const breadcrumbs = group && (
    <Row>
      <Col md={12}>
        <Breadcrumbs items={[{
          label: 'Group',
          text: group.id,
          link: `group/${group.id}`
        }]}
        />
      </Col>
    </Row>
  );
  return (
    <header className="detail-header">
      {breadcrumbs}
      <Row>
        <Col md={7} lg={6}>
          <RequestTitle requestId={requestId} />
        </Col>
        <Col md={5} lg={6} className="button-container">
          <RequestActionButtons requestId={requestId} />
        </Col>
      </Row>
      <Row>
        <Col md={12}>
          <RequestAlerts requestId={requestId} />
        </Col>
      </Row>
    </header>
  );
};

RequestHeader.propTypes = {
  requestId: PropTypes.string.isRequired,
  group: PropTypes.object
};

function mapStateToProps(state, ownProps) {
  return {
    group: _.first(_.filter(state.api.requestGroups.data, (g) => _.contains(g.requestIds, ownProps.requestId)))
  };
}

export default connect(mapStateToProps)(RequestHeader);
