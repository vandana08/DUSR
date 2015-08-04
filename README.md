# DUSR
                                  Distributed Ultrafast shape recognition
Ligand-based virtual screening to identify potential new drugs requires both management of vast amount of available data and heavy computational facility. The Ultrafast Shape Recognition (USR) algorithm using structural similarity is an important aid to find novel drugs in chemical databases. The algorithm however was devoid of potential to discriminate similar shaped compounds possessing different pharmacophoric features. To overcome this discrepancy, a modification in the existing USR algorithm called DUSR (Distributed Ultrafast Shape Recognition) was done in which compounds were screened on the basis of their drug-likeliness properties prior to molecular shape comparison. The DUSR is far better than the existing original method whilst screening the compounds on the basis of Lipinski’s rule. DUSR also implements MapReduce algorithm supporting high throughput screening of million conformers in a reduced time span. We demonstrated the utility of DUSR on different number of molecules by running job on two platforms- (a) JAVA and (b) Hadoop (standalone mode, 3-node & 5-node fully distributed mode). The result demonstrated that DUSR completed its job in 4541, 1421, 802, seconds respectively for 2,038,924 molecules on Hadoop (standalone mode, 3-node & 5-node fully distributed mode) which is faster than USR who have used a small sized data viz. 666,892 molecules. In addition, DUSR is approximately five fold faster than the existing method and is especially suitable for Hadoop > 5-node Fully Distributed mode.
