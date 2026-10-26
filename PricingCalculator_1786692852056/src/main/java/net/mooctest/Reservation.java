package net.mooctest;

import java.time.LocalDate;
import java.time.Month;

public class Reservation {
	private Guest guest;
	private RoomType roomType;
	private LocalDate checkInDate;
	private int nights;             // 入住天数
	private int extraBeds;          // 加床数
	private int breakfastPeople;    // 含早餐人数

	public Reservation(Guest guest, RoomType roomType, LocalDate checkInDate,
					   int nights, int extraBeds, int breakfastPeople) {
		this.guest = guest;
		this.roomType = roomType;
		this.checkInDate = checkInDate;
		this.nights = nights;
		this.extraBeds = extraBeds;
		this.breakfastPeople = breakfastPeople;
	}

	// 获取入住月份（用于季节判断）
	public Month getCheckInMonth() {
		return checkInDate.getMonth();
	}

	// Getter
	public Guest getGuest() { return guest; }
	public RoomType getRoomType() { return roomType; }
	public LocalDate getCheckInDate() { return checkInDate; }
	public int getNights() { return nights; }
	public int getExtraBeds() { return extraBeds; }
	public int getBreakfastPeople() { return breakfastPeople; }
}