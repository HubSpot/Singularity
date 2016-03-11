# Releases

{% for release in book.releases %}
  - [Changes in {{release}}](#changes-in-{{release.replace(".", "").replace(".", "")}}){% endfor %}

{% for release in book.releases %}
  {% set file = "./" + release + ".md" %}
  {% include file %}
* * *
{% endfor %}