package com.example.truyendex.data.repository;

import androidx.annotation.DrawableRes;

import com.example.truyendex.R;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.model.Novel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// kho du lieu truyen
public class NovelRepository {

    private static NovelRepository instance;
    private final List<Novel> novels;
    private final Map<String, Integer> sampleCoversByName = new HashMap<>();

    public static NovelRepository getInstance() {
        if (instance == null) {
            instance = new NovelRepository();
        }
        return instance;
    }

    // cap nhat danh sach truyen tu sqlite vao danh sach hien thi
    public void loadNovelsFromDb(AppDatabase db) {
        if (db == null) return;
        List<Novel> dbList = db.getAllNovelsFromDb();
        for (Novel item : dbList) {
            boolean found = false;
            for (int i = 0; i < novels.size(); i++) {
                if (novels.get(i).getId() == item.getId()) {
                    novels.set(i, item);
                    found = true;
                    break;
                }
            }
            if (!found) {
                novels.add(0, item);
            }
        }
    }

    private NovelRepository() {
        novels = new ArrayList<>();
        novels.add(new Novel(1, "Khu rừng của những lá thư", "An Nhiên", "Mộc",
                Arrays.asList("Fantasy", "Đời thường"), 18, "Đang tiến hành", "Sáng tác",
                R.drawable.cover_01_khu_rung_cua_nhung_la_thu,
                12, 920, 4200, 12400));
        novels.add(new Novel(2, "Hẹn em ở mùa hạ năm ấy", "Hạ Lam", "Mây",
                Arrays.asList("Romance", "Học đường"), 48, "Hoàn thành", "Truyện dịch",
                R.drawable.cover_02_hen_em_o_mua_ha_nam_ay,
                5, 1500, 5800, 46000));
        novels.add(new Novel(3, "Phía bên kia bầu trời", "Lâm Vũ", "Sora",
                Arrays.asList("Fantasy", "Phiêu lưu"), 126, "Đang tiến hành", "Truyện dịch",
                R.drawable.cover_03_phia_ben_kia_bau_troi,
                11, 1800, 6100, 52000));
        novels.add(new Novel(4, "Tiệm sách cuối con ngõ nhỏ", "An Nhiên", "Mộc",
                Arrays.asList("Đời thường", "Romance"), 24, "Đang tiến hành", "Sáng tác",
                R.drawable.cover_04_tiem_sach_cuoi_con_ngo_nho,
                10, 820, 3000, 15000));
        novels.add(new Novel(5, "Người giữ những vì sao", "Lâm Vũ", "Sora",
                Arrays.asList("Fantasy", "Hành động"), 86, "Đang tiến hành", "AI dịch",
                R.drawable.cover_05_nguoi_giu_nhung_vi_sao,
                9, 1700, 5200, 38000));
        novels.add(new Novel(6, "Ngày gió ghé qua hiên nhà", "Hạ Lam", "Mây",
                Arrays.asList("Đời thường", "Drama"), 32, "Hoàn thành", "Sáng tác",
                R.drawable.cover_06_ngay_gio_ghe_qua_hien_nha,
                4, 650, 2400, 18000));
        novels.add(new Novel(7, "Học viện của những giấc mơ", "Minh Kha", "Bút Chì",
                Arrays.asList("Học đường", "Hài hước"), 64, "Đang tiến hành", "Truyện dịch",
                R.drawable.cover_07_hoc_vien_cua_nhung_giac_mo,
                8, 1200, 5500, 41000));
        novels.add(new Novel(8, "Chuyến tàu lúc nửa đêm", "Thanh Vân", "Mộc",
                Arrays.asList("Bí ẩn", "Siêu nhiên"), 40, "Hoàn thành", "Truyện dịch",
                R.drawable.cover_08_chuyen_tau_luc_nua_dem,
                3, 980, 4600, 27000));
        novels.add(new Novel(9, "Mùa xuân của pháp sư tập sự", "Minh Kha", "Mây",
                Arrays.asList("Fantasy", "Hài hước"), 72, "Đang tiến hành", "AI dịch",
                R.drawable.cover_09_mua_xuan_cua_phap_su_tap_su,
                7, 1320, 4800, 32000));
        novels.add(new Novel(10, "Thành phố sau cơn mưa", "Thanh Vân", "Bút Chì",
                Arrays.asList("Khoa học viễn tưởng", "Hành động"), 56, "Hoàn thành", "Sáng tác",
                R.drawable.cover_10_thanh_pho_sau_con_mua,
                2, 760, 3900, 49000));
        novels.add(new Novel(11, "Lời hẹn dưới tán ngân hạnh", "Hạ Lam", "Sora",
                Arrays.asList("Lịch sử", "Romance"), 36, "Đang tiến hành", "Truyện dịch",
                R.drawable.cover_11_loi_hen_duoi_tan_ngan_hanh,
                6, 1100, 4400, 23000));
        novels.add(new Novel(12, "Bản đồ những vùng đất quên", "Lâm Vũ", "Bút Chì",
                Arrays.asList("Phiêu lưu", "Bí ẩn"), 90, "Hoàn thành", "AI dịch",
                R.drawable.cover_12_ban_do_nhung_vung_dat_quen,
                1, 890, 3400, 57000));
        for (Novel novel : novels) {
            sampleCoversByName.put(novel.getName(), novel.getCoverImageResId());
        }
    }

    @DrawableRes
    public int getSampleCoverImageResId(String novelName) {
        return sampleCoversByName.getOrDefault(novelName, 0);
    }

    public List<Novel> getAllNovels() {
        return new ArrayList<>(novels);
    }

    public List<Novel> getFeaturedNovels() {
        // lay 4 truyen dau lam truyen de xuat
        return new ArrayList<>(novels.subList(0, Math.min(4, novels.size())));
    }

    public Novel getNovelById(int id) {
        for (Novel n : novels) {
            if (n.getId() == id) return n;
        }
        return null;
    }

    // sap xep bang xep hang theo tuan, thang hoac tat ca
    public List<Novel> getRanking(String period) {
        List<Novel> sorted = new ArrayList<>(novels);
        Comparator<Novel> cmp;
        switch (period) {
            case "week":
                cmp = (a, b) -> Integer.compare(b.getWeekViews(), a.getWeekViews());
                break;
            case "month":
                cmp = (a, b) -> Integer.compare(b.getMonthViews(), a.getMonthViews());
                break;
            default:
                cmp = (a, b) -> Integer.compare(b.getAllViews(), a.getAllViews());
                break;
        }
        Collections.sort(sorted, cmp);
        return sorted.subList(0, Math.min(5, sorted.size()));
    }

    public List<String> getGenres() {
        return Arrays.asList(
                "Fantasy", "Romance", "Đời thường", "Học đường",
                "Hành động", "Phiêu lưu", "Hài hước", "Bí ẩn",
                "Drama", "Siêu nhiên", "Khoa học viễn tưởng", "Lịch sử"
        );
    }
}
