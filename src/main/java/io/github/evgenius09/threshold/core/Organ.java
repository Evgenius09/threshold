package io.github.evgenius09.threshold.core;

public enum Organ {
	SHADOW_GLAND(Element.SHADOW),
	THUNDER_FANG(Element.STORM);

	private final Element element;

	Organ(Element element) {
		this.element = element;
	}

	public Element element() {
		return element;
	}
}
