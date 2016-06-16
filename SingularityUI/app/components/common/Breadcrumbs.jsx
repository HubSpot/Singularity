import React from 'react';

export default class Breadcrumbs extends React.Component {

  renderItems() {
    return this.props.items.map((i, n) => {
      if (i.link) {
        return (
          <li key={n}>{i.label} <a href={i.link}>{i.text}</a></li>
        );
      } else if (i.onClick) {
        return (
          <li key={n}>{i.label} <a onClick={i.onClick}>{i.text}</a></li>
        );
      } else {
        return (
          <li key={n}>{i.label} {i.text}</li>
        );
      }
    });
  }

  render() {
    return (
      <ul className="breadcrumb">
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
