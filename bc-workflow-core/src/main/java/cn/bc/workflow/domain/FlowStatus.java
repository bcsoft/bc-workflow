package cn.bc.workflow.domain;

import cn.bc.core.EnumWithValue;

/**
 * 流转状态
 *
 * @author dragon 2016-05-24.
 */
public enum FlowStatus implements EnumWithValue {
	/**
	 * 未流转，如未发起流程
	 */
	None(0, "无"),

	/**
	 * 流转中
	 */
	Flowing(1, "流转中"),

	/**
	 * 已暂停
	 */
	Paused(2, "已暂停"),

	/**
	 * 已结束
	 */
	Finished(3, "已结束");

	private final int value;       // 值
	private final String label;    // 名称

	FlowStatus(int value, String label) {
		this.value = value;
		this.label = label;
	}

	@Override
	public int value() {
		return value;
	}

	/**
	 * 获取名称
	 */
	public String label() {
		return label;
	}

	/**
	 * 获取持久化值对应的权举值
	 *
	 * @throws IllegalArgumentException 如果指定的值不存在
	 */
	public static FlowStatus valueOf(int value) {
		for (FlowStatus t : FlowStatus.values()) {
			if (t.value() == value) return t;
		}
		throw new IllegalArgumentException("unsupported FlowStatus value: " + value);
	}
}