import React, { PropTypes } from 'react';

import { Row, Col } from 'react-bootstrap';

import RequestTitle from './header/RequestTitle';
import RequestActionButtons from './header/RequestActionButtons';
import RequestAlerts from './header/RequestAlerts';

const RequestHeader = ({requestId}) => (
  <header className="detail-header">
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

RequestHeader.propTypes = {
  requestId: PropTypes.string.isRequired
};

export default RequestHeader;
