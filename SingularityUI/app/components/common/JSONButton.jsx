import React, { Component, PropTypes } from 'react';
import Modal from 'react-bootstrap/lib/Modal';
import Button from 'react-bootstrap/lib/Button';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import JSONTree from 'react-json-tree';
import { JSONTreeTheme } from '../../thirdPartyConfigurations';
import Clipboard from 'clipboard';

export default class JSONButton extends Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node
    ]).isRequired,
    object: PropTypes.object.isRequired,
    showOverlay: PropTypes.bool,
    className: PropTypes.string,
    linkClassName: PropTypes.string
  };

  constructor() {
    super();
    this.state = {
      modalOpen: false
    };

    this.showJSON = this.showJSON.bind(this);
    this.hideJSON = this.hideJSON.bind(this);
  }

  componentDidMount() {
    this.clipboard = new Clipboard('.copy-btn');
  }

  showJSON() {
    this.setState({
      modalOpen: true
    });
  }

  hideJSON() {
    this.setState({
      modalOpen: false
    });
  }

  render() {
    const jsonTooltip = (
      <ToolTip id="view-json-tooltip">
        JSON
      </ToolTip>
    );
    const button = (
      <a className={this.props.linkClassName} onClick={this.showJSON} alt="Show JSON">{this.props.children}</a>
    );
    return (
      <span className={this.props.className}>
        {this.props.showOverlay ? (
          <OverlayTrigger placement="top" id="view-json-overlay" overlay={jsonTooltip}>
            {button}
          </OverlayTrigger>) : button
        }
        <Modal show={this.state.modalOpen} onHide={this.hideJSON} bsSize="large">
          <Modal.Body>
            <div className="constrained-modal json-modal">
              <JSONTree
                data={this.props.object}
                shouldExpandNode={() => true}
                theme={JSONTreeTheme}
              />
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button bsStyle="default" className="copy-btn" data-clipboard-text={JSON.stringify(this.props.object, null, 2)}>Copy</Button>
            <Button bsStyle="info" onClick={this.hideJSON}>Close</Button>
          </Modal.Footer>
        </Modal>
      </span>
    );
  }
}
