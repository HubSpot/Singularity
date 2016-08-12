import React from 'react';
import { Link } from 'react-router';

export default class Breadcrumbs extends React.Component {

  renderItems() {
    return this.props.items.map((i, n) => {
      if (i.link) {
        return (
          <li key={n}>{i.label} <Link to={i.link}>{i.text}</Link></li>
        );
      } else if (i.onClick) {
        return (
          <li key={n}>{i.label} <a onClick={i.onClick}>{i.text}</a></li>
        );
      }
      return (
        <li key={n}>{i.label} {i.text}</li>
      );
    });
  }

  render() {
    return (
      <ul className="breadcrumb clearfix">
          {this.renderItems()}
          <span className="pull-right">{this.props.right}</span>
      </ul>
    );
  }
}

Breadcrumbs.propTypes = {
  items: React.PropTypes.arrayOf(React.PropTypes.shape({
    label: React.PropTypes.string,
    text: React.PropTypes.oneOfType([React.PropTypes.string.isRequired, React.PropTypes.number.isRequired]),
    link: React.PropTypes.string
  })).isRequired,
  right: React.PropTypes.element
};
