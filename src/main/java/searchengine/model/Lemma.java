package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "lemma", indexes = {@Index(name = "lemma_list", columnList = "lemma")})
@NoArgsConstructor
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site siteId;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<searchengine.model.Index> index = new ArrayList<>();

    public Lemma(Site siteId, String lemma, int frequency) {
        this.siteId = siteId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return id == lemma1.id && frequency == lemma1.frequency && Objects.equals(siteId, lemma1.siteId) && Objects.equals(lemma, lemma1.lemma) && Objects.equals(index, lemma1.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteId, lemma, frequency, index);
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                ", index=" + index +
                '}';
    }
}
